export interface CareerSummary {
  code: string
  name: string
  institution: string
  totalCredits: number
  courseCount: number
}

export interface Course {
  code: string
  name: string
  credits: number
  term: number
  requires: string[]
  unlocks: number
}

export interface Curriculum {
  career: CareerSummary
  courses: Course[]
}

export interface Bottleneck {
  code: string
  name: string
  term: number
  unlocks: number
  credits: number
}

export interface Insights {
  careerCode: string
  minTerms: number
  courseCount: number
  totalCredits: number
  bottlenecks: Bottleneck[]
}

export interface SimulationRequest {
  careerCode: string
  approved: string[]
  failed: string[]
  maxCreditsPerTerm: number
  save?: boolean
}

export interface PlannedCourse {
  code: string
  name: string
  credits: number
  suggestedTerm: number
  retake: boolean
}

export interface PlannedTerm {
  index: number
  credits: number
  courses: PlannedCourse[]
}

export interface Simulation {
  careerCode: string
  minTerms: number
  baselineTerms: number
  totalTerms: number
  delay: number
  totalCredits: number
  unscheduled: string[]
  terms: PlannedTerm[]
}

export type CourseState = 'approved' | 'failed' | 'available' | 'blocked'

export interface SeedCourse {
  code: string
  name: string
  credits: number
  term: number
  requires?: string[]
}

export interface SeedPayload {
  code: string
  name: string
  institution: string
  totalCredits: number
  courses: SeedCourse[]
}
