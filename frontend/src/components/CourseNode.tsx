import { Handle, Position, type Node, type NodeProps } from '@xyflow/react'
import type { CourseState } from '../types'

export interface CourseNodeData extends Record<string, unknown> {
  code: string
  name: string
  credits: number
  term: number
  unlocks: number
  state: CourseState
}

export type CourseFlowNode = Node<CourseNodeData, 'course'>

export function CourseNodeView({ data }: NodeProps<CourseFlowNode>) {
  return (
    <div className={`course-node state-${data.state}`} title={`${data.code} · ${data.name}`}>
      <Handle type="target" position={Position.Left} className="course-handle" />
      <div className="course-node__top">
        <span className="course-node__code">{data.code}</span>
        <span className="course-node__credits">{data.credits} cr</span>
      </div>
      <div className="course-node__name">{data.name}</div>
      <div className="course-node__bottom">
        <span>Semestre {data.term}</span>
        {data.unlocks > 0 && <span className="course-node__unlocks">↳ {data.unlocks}</span>}
      </div>
      <Handle type="source" position={Position.Right} className="course-handle" />
    </div>
  )
}
