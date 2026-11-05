import type {
  Curriculum,
  Insights,
  SeedPayload,
  Simulation,
  SimulationRequest,
} from './types'

const BASE = import.meta.env.VITE_API_URL ?? ''

export const docsUrl = `${BASE || 'http://localhost:8080'}/docs`

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE}${url}`, init)
  if (!response.ok) {
    let detail = `Error ${response.status}`
    try {
      const body = await response.json()
      detail = body.message ?? detail
    } catch {
      /* respuesta sin cuerpo JSON */
    }
    throw new Error(detail)
  }
  return response.json() as Promise<T>
}

export const api = {
  curriculum: (code: string) => request<Curriculum>(`/api/careers/${code}/curriculum`),
  insights: (code: string) => request<Insights>(`/api/careers/${code}/insights`),
  simulate: (payload: SimulationRequest) =>
    request<Simulation>('/api/planner/simulate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }),
}

/**
 * Modo demo: si el backend no esta levantado, la UI sigue mostrando la malla
 * leyendo el mismo JSON semilla. La simulacion se desactiva con un aviso claro
 * en vez de mentir con datos calculados en el cliente.
 */
export async function loadDemoCurriculum(): Promise<Curriculum> {
  const seed = await request<SeedPayload>('/malla-ti-espe.json')
  const courses = seed.courses
  const unlocks = new Map<string, number>()

  const dependents = new Map<string, string[]>()
  courses.forEach((course) => {
    ;(course.requires ?? []).forEach((required) => {
      dependents.set(required, [...(dependents.get(required) ?? []), course.code])
    })
  })

  courses.forEach((course) => {
    const seen = new Set<string>()
    const queue = [...(dependents.get(course.code) ?? [])]
    while (queue.length > 0) {
      const current = queue.shift() as string
      if (!seen.has(current)) {
        seen.add(current)
        queue.push(...(dependents.get(current) ?? []))
      }
    }
    unlocks.set(course.code, seen.size)
  })

  return {
    career: {
      code: seed.code,
      name: seed.name,
      institution: seed.institution,
      totalCredits: seed.totalCredits,
      courseCount: courses.length,
    },
    courses: courses.map((course) => ({
      code: course.code,
      name: course.name,
      credits: course.credits,
      term: course.term,
      requires: course.requires ?? [],
      unlocks: unlocks.get(course.code) ?? 0,
    })),
  }
}
