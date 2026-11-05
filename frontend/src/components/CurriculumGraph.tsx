import { useMemo } from 'react'
import {
  Background,
  BackgroundVariant,
  Controls,
  MiniMap,
  ReactFlow,
  type Edge,
} from '@xyflow/react'
import { CourseNodeView, type CourseFlowNode } from './CourseNode'
import type { Course, CourseState } from '../types'

const nodeTypes = { course: CourseNodeView }

const PALETTE: Record<CourseState, string> = {
  approved: '#34d399',
  failed: '#fb7185',
  available: '#38bdf8',
  blocked: '#475569',
}

interface Props {
  courses: Course[]
  stateOf: (code: string) => CourseState
  onToggle: (code: string) => void
}

export function CurriculumGraph({ courses, stateOf, onToggle }: Props) {
  const nodes = useMemo<CourseFlowNode[]>(() => {
    const byTerm = new Map<number, Course[]>()
    courses.forEach((course) => {
      byTerm.set(course.term, [...(byTerm.get(course.term) ?? []), course])
    })

    const result: CourseFlowNode[] = []
    ;[...byTerm.entries()]
      .sort(([a], [b]) => a - b)
      .forEach(([term, list]) => {
        list
          .slice()
          .sort((a, b) => a.code.localeCompare(b.code))
          .forEach((course, index) => {
            result.push({
              id: course.code,
              type: 'course',
              position: {
                x: (term - 1) * 300,
                y: (index - (list.length - 1) / 2) * 118,
              },
              data: {
                code: course.code,
                name: course.name,
                credits: course.credits,
                term: course.term,
                unlocks: course.unlocks,
                state: stateOf(course.code),
              },
            })
          })
      })
    return result
  }, [courses, stateOf])

  const edges = useMemo<Edge[]>(
    () =>
      courses.flatMap((course) =>
        course.requires.map((required) => {
          const requiredState = stateOf(required)
          const targetState = stateOf(course.code)
          const done = requiredState === 'approved' && targetState === 'approved'
          const active = requiredState === 'approved' && targetState === 'available'
          return {
            id: `${required}->${course.code}`,
            source: required,
            target: course.code,
            type: 'smoothstep',
            animated: active,
            style: {
              stroke: done ? 'rgba(52,211,153,0.55)' : active ? 'rgba(56,189,248,0.5)' : 'rgba(148,163,184,0.2)',
              strokeWidth: done || active ? 1.7 : 1.1,
            },
          }
        }),
      ),
    [courses, stateOf],
  )

  return (
    <div className="graph-canvas">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        fitView
        fitViewOptions={{ padding: 0.12 }}
        minZoom={0.15}
        maxZoom={1.4}
        nodesDraggable
        nodesConnectable={false}
        elementsSelectable={false}
        onNodeClick={(_, node) => onToggle(node.id)}
        className="flow"
      >
        <Background variant={BackgroundVariant.Dots} gap={26} size={1} color="rgba(148,163,184,0.16)" />
        <Controls showInteractive={false} position="bottom-left" />
        <MiniMap
          pannable
          zoomable
          nodeColor={(node) => PALETTE[(node.data as CourseFlowNode['data']).state] ?? '#475569'}
          maskColor="rgba(4,8,16,0.7)"
          position="bottom-right"
        />
      </ReactFlow>
    </div>
  )
}
