import type { Insights, PlannedTerm, Simulation } from '../types'

interface Props {
  insights: Insights | null
  sim: Simulation | null
  approvedCount: number
  failedCount: number
  maxCredits: number
  onMaxCredits: (value: number) => void
  mode: 'approve' | 'fail'
  onMode: (mode: 'approve' | 'fail') => void
  onReset: () => void
  onPreset: () => void
  onSave: () => void
  saveState: 'idle' | 'saving' | 'saved'
  offline: boolean
  error: string | null
}

export function Sidebar({
  insights,
  sim,
  approvedCount,
  failedCount,
  maxCredits,
  onMaxCredits,
  mode,
  onMode,
  onReset,
  onPreset,
  onSave,
  saveState,
  offline,
  error,
}: Props) {
  const topUnlocks = insights?.bottlenecks[0]?.unlocks ?? 1

  const planRows: Array<{ index: number; term?: PlannedTerm }> = []
  if (sim) {
    let expected = 1
    for (const term of sim.terms) {
      while (expected < term.index) {
        planRows.push({ index: expected })
        expected += 1
      }
      planRows.push({ index: term.index, term })
      expected = term.index + 1
    }
  }

  return (
    <aside className="sidebar">
      <section className="panel">
        <div className="panel__head">
          <h2>Simulador</h2>
          <span className="hint">Clic en una materia</span>
        </div>

        <div className="segmented" role="group" aria-label="Acción al hacer clic">
          <button
            type="button"
            className={mode === 'approve' ? 'is-active' : ''}
            onClick={() => onMode('approve')}
          >
            Aprobar
          </button>
          <button
            type="button"
            className={mode === 'fail' ? 'is-active is-danger' : ''}
            onClick={() => onMode('fail')}
          >
            Perder
          </button>
        </div>

        <div className="field">
          <label htmlFor="credits">
            Tope de créditos por semestre <strong>{maxCredits}</strong>
          </label>
          <input
            id="credits"
            type="range"
            min={6}
            max={30}
            step={1}
            value={maxCredits}
            onChange={(event) => onMaxCredits(Number(event.target.value))}
          />
        </div>

        <div className="counters">
          <div className="counter counter--ok">
            <strong>{approvedCount}</strong>
            <span>Aprobadas</span>
          </div>
          <div className="counter counter--bad">
            <strong>{failedCount}</strong>
            <span>Perdidas</span>
          </div>
        </div>

        <div className="actions">
          <button type="button" onClick={onPreset}>
            Alumno de 5.º semestre
          </button>
          <button type="button" className="ghost" onClick={onReset}>
            Limpiar
          </button>
        </div>
      </section>

      {offline ? (
        <section className="panel notice">
          <h3>Modo lectura</h3>
          <p>
            El backend no está disponible, así que el grafo se muestra desde el JSON semilla.
            Levanta la API para activar la simulación.
          </p>
        </section>
      ) : error ? (
        <section className="panel notice notice--error">
          <h3>No se pudo simular</h3>
          <p>{error}</p>
        </section>
      ) : sim ? (
        <>
          <section className="panel result">
            <div className="result__head">
              <div>
                <span className="result__label">Plan simulado</span>
                <div className="result__value">
                  {sim.totalTerms}
                  <span className="result__unit">semestres</span>
                </div>
              </div>
              {sim.delay > 0 && <span className="badge badge--warn">+{sim.delay} por pérdidas</span>}
            </div>

            <div className="result__compare">
              <div>
                <span>Mínimo teórico</span>
                <strong>{sim.minTerms} sem.</strong>
              </div>
              <div>
                <span>Sin perder nada</span>
                <strong>{sim.baselineTerms} sem.</strong>
              </div>
              <div>
                <span>Créditos</span>
                <strong>{sim.totalCredits}</strong>
              </div>
            </div>

            {sim.unscheduled.length > 0 && (
              <div className="badge badge--danger">
                {sim.unscheduled.length} materias sin ubicar: {sim.unscheduled.join(', ')}
              </div>
            )}
          </section>

          <section className="panel">
            <div className="panel__head">
              <h2>Cuellos de botella</h2>
              <span className="hint">materias que más desbloquean</span>
            </div>
            <ul className="bottlenecks">
              {(insights?.bottlenecks ?? []).map((item) => (
                <li key={item.code}>
                  <div className="bottleneck__row">
                    <span className="bottleneck__name">{item.name}</span>
                    <span className="bottleneck__value">{item.unlocks}</span>
                  </div>
                  <div className="bar">
                    <div
                      className="bar__fill"
                      style={{ width: `${Math.max(6, (item.unlocks / topUnlocks) * 100)}%` }}
                    />
                  </div>
                </li>
              ))}
            </ul>
          </section>

          <section className="panel">
            <div className="panel__head">
              <h2>Plan semestre por semestre</h2>
              <button type="button" className="ghost ghost--small" onClick={onSave}>
                {saveState === 'saving' ? 'Guardando…' : saveState === 'saved' ? 'Guardado ✓' : 'Guardar'}
              </button>
            </div>
            <p className="plan__hint">Contando desde tu próximo semestre.</p>
            <div className="plan">
              {planRows.map((row) =>
                row.term ? (
                  <div className="term" key={row.index}>
                    <div className="term__head">
                      <span>Semestre {row.index}</span>
                      <span className="term__credits">{row.term.credits} cr</span>
                    </div>
                    <div className="term__courses">
                      {row.term.courses.map((course) => (
                        <span
                          className={`chip ${course.retake ? 'chip--retake' : ''}`}
                          key={course.code}
                          title={`${course.code} · ${course.name}`}
                        >
                          {course.name}
                          {course.retake && <em>recursa</em>}
                        </span>
                      ))}
                    </div>
                  </div>
                ) : (
                  <div className="term term--gap" key={row.index}>
                    <div className="term__head">
                      <span>Semestre {row.index}</span>
                      <span>libre · esperando prerrequisito</span>
                    </div>
                  </div>
                ),
              )}
            </div>
          </section>
        </>
      ) : (
        <section className="panel skeleton">
          <div className="skeleton__line" />
          <div className="skeleton__line skeleton__line--short" />
          <div className="skeleton__block" />
        </section>
      )}
    </aside>
  )
}
