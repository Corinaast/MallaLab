import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { api, docsUrl, loadDemoCurriculum } from './api'
import { CurriculumGraph } from './components/CurriculumGraph'
import { Sidebar } from './components/Sidebar'
import type { Course, CourseState, Curriculum, Insights, Simulation } from './types'

const CAREER_CODE = 'TI-ESPE'

export default function App() {
  const [curriculum, setCurriculum] = useState<Curriculum | null>(null)
  const [insights, setInsights] = useState<Insights | null>(null)
  const [offline, setOffline] = useState(false)
  const [loading, setLoading] = useState(true)

  const [approved, setApproved] = useState<Set<string>>(new Set())
  const [failed, setFailed] = useState<Set<string>>(new Set())
  const [maxCredits, setMaxCredits] = useState(18)
  const [mode, setMode] = useState<'approve' | 'fail'>('approve')

  const [sim, setSim] = useState<Simulation | null>(null)
  const [simError, setSimError] = useState<string | null>(null)
  const [saveState, setSaveState] = useState<'idle' | 'saving' | 'saved'>('idle')

  const presetApplied = useRef(false)

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        const [cur, ins] = await Promise.all([
          api.curriculum(CAREER_CODE),
          api.insights(CAREER_CODE),
        ])
        if (cancelled) return
        setCurriculum(cur)
        setInsights(ins)
      } catch {
        if (cancelled) return
        try {
          const demo = await loadDemoCurriculum()
          if (cancelled) return
          setCurriculum(demo)
          setOffline(true)
        } catch {
          setSimError('No se pudo cargar la malla')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!curriculum || presetApplied.current) return
    presetApplied.current = true
    setApproved(new Set(curriculum.courses.filter((course) => course.term <= 4).map((c) => c.code)))
  }, [curriculum])

  useEffect(() => {
    if (!curriculum || offline) return
    const timer = setTimeout(() => {
      api
        .simulate({
          careerCode: CAREER_CODE,
          approved: [...approved],
          failed: [...failed],
          maxCreditsPerTerm: maxCredits,
        })
        .then((result) => {
          setSim(result)
          setSimError(null)
        })
        .catch((error: Error) => setSimError(error.message))
    }, 220)
    return () => clearTimeout(timer)
  }, [approved, failed, maxCredits, curriculum, offline])

  const courseByCode = useMemo(() => {
    const map = new Map<string, Course>()
    curriculum?.courses.forEach((course) => map.set(course.code, course))
    return map
  }, [curriculum])

  const dependentsByCode = useMemo(() => {
    const map = new Map<string, string[]>()
    curriculum?.courses.forEach((course) => {
      course.requires.forEach((required) => {
        map.set(required, [...(map.get(required) ?? []), course.code])
      })
    })
    return map
  }, [curriculum])

  /**
   * Si se desmarca una materia, sus dependientes tampoco pueden seguir aprobadas:
   * el historial tiene que ser consistente con los prerrequisitos.
   */
  const unapproveWithDependents = useCallback(
    (previous: Set<string>, code: string) => {
      const next = new Set(previous)
      const stack = [code]
      const seen = new Set<string>()
      while (stack.length > 0) {
        const current = stack.pop() as string
        if (seen.has(current)) continue
        seen.add(current)
        next.delete(current)
        stack.push(...(dependentsByCode.get(current) ?? []))
      }
      return next
    },
    [dependentsByCode],
  )

  const stateOf = useCallback(
    (code: string): CourseState => {
      if (approved.has(code)) return 'approved'
      if (failed.has(code)) return 'failed'
      const course = courseByCode.get(code)
      if (!course) return 'blocked'
      return course.requires.every((required) => approved.has(required)) ? 'available' : 'blocked'
    },
    [approved, failed, courseByCode],
  )

  const toggle = useCallback(
    (code: string) => {
      if (mode === 'fail') {
        if (failed.has(code)) {
          setFailed((previous) => {
            const next = new Set(previous)
            next.delete(code)
            return next
          })
        } else {
          setFailed((previous) => new Set(previous).add(code))
          setApproved((previous) => unapproveWithDependents(previous, code))
        }
      } else {
        if (approved.has(code)) {
          setApproved((previous) => unapproveWithDependents(previous, code))
        } else {
          setApproved((previous) => new Set(previous).add(code))
        }
        setFailed((previous) => {
          if (!previous.has(code)) return previous
          const next = new Set(previous)
          next.delete(code)
          return next
        })
      }
      setSaveState('idle')
    },
    [mode, approved, failed, unapproveWithDependents],
  )

  const applyPreset = useCallback(() => {
    if (!curriculum) return
    setApproved(new Set(curriculum.courses.filter((course) => course.term <= 4).map((c) => c.code)))
    setFailed(new Set())
  }, [curriculum])

  const reset = useCallback(() => {
    setApproved(new Set())
    setFailed(new Set())
  }, [])

  const savePlan = useCallback(async () => {
    if (!curriculum || offline) return
    setSaveState('saving')
    try {
      await api.simulate({
        careerCode: CAREER_CODE,
        approved: [...approved],
        failed: [...failed],
        maxCreditsPerTerm: maxCredits,
        save: true,
      })
      setSaveState('saved')
      setTimeout(() => setSaveState('idle'), 2400)
    } catch {
      setSaveState('idle')
    }
  }, [approved, failed, maxCredits, curriculum, offline])

  const stats = {
    courses: curriculum?.career.courseCount ?? 0,
    credits: curriculum?.career.totalCredits ?? 0,
    minTerms: insights?.minTerms ?? null,
    planTerms: sim?.totalTerms ?? null,
  }

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <div className="brand__mark">ML</div>
          <div className="brand__text">
            <h1>MallaLab</h1>
            <p>{curriculum?.career.institution ?? 'Simulador de trayectoria académica'}</p>
          </div>
        </div>

        <div className="stats">
          <div className="stat">
            <span>Materias</span>
            <strong>{stats.courses || '—'}</strong>
          </div>
          <div className="stat">
            <span>Créditos</span>
            <strong>{stats.credits || '—'}</strong>
          </div>
          <div className="stat">
            <span>Mínimo teórico</span>
            <strong>{stats.minTerms ?? '—'}</strong>
          </div>
          <div className="stat stat--accent">
            <span>Te faltan</span>
            <strong>{stats.planTerms ? `${stats.planTerms} sem.` : '—'}</strong>
          </div>
        </div>

        <a className="ghost-link" href={docsUrl} target="_blank" rel="noreferrer">
          API docs ↗
        </a>
      </header>

      {offline && (
        <div className="banner">
          Modo lectura: la API está apagada, así que solo se muestra el grafo. Arranca el backend para
          simular escenarios.
        </div>
      )}

      <main className="workspace">
        <section className="graph-pane">
          <div className="graph-pane__head">
            <div>
              <h2>{curriculum?.career.name ?? 'Cargando malla…'}</h2>
              <p className="muted">
                Cada nodo es una materia; cada flecha, un prerrequisito. Clic para aprobar o perder.
              </p>
            </div>
            <div className="legend">
              <span className="legend__item legend__item--approved">Aprobada</span>
              <span className="legend__item legend__item--available">Disponible</span>
              <span className="legend__item legend__item--blocked">Bloqueada</span>
              <span className="legend__item legend__item--failed">Perdida</span>
            </div>
          </div>

          <div className="graph-pane__body">
            {loading && <div className="loading">Cargando malla…</div>}
            {!loading && curriculum && (
              <CurriculumGraph courses={curriculum.courses} stateOf={stateOf} onToggle={toggle} />
            )}
            {!loading && !curriculum && <div className="loading">No se pudo cargar la malla.</div>}
          </div>
        </section>

        <Sidebar
          insights={insights}
          sim={sim}
          approvedCount={approved.size}
          failedCount={failed.size}
          maxCredits={maxCredits}
          onMaxCredits={setMaxCredits}
          mode={mode}
          onMode={setMode}
          onReset={reset}
          onPreset={applyPreset}
          onSave={savePlan}
          saveState={saveState}
          offline={offline}
          error={simError}
        />
      </main>
    </div>
  )
}
