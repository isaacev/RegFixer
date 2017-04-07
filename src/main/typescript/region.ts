//
// src/main/typescript/region.ts
// RegEx Frontend
//
// Created on 2/20/17
//

import 'codemirror'
import { CorpusEditor } from './corpus-editor'
import { RegionLink } from './region-list'
import { LeftGrip, RightGrip } from './grip'
import { Point } from './point'
import * as util from './util'

function positionToString (pt: CodeMirror.Position): string {
  return `(${pt.line}:${pt.ch})`
}

export class Region {
  editor: CorpusEditor
  left: LeftGrip
  right: RightGrip
  link: RegionLink
  color: string

  onMove: (left: Point, right: Point) => void = util.noop

  constructor (editor: CorpusEditor, start: Point, end: Point, color: string) {
    this.editor = editor
    this.left = new LeftGrip(this, start, color)
    this.right = new RightGrip(this, end, color)
    this.color = color
  }

  get start (): Point {
    return this.left.point
  }

  set start (point: Point) {
    this.left.point = point
    this.left.updatePosition()
    this.onMove(this.start, this.end)
  }

  get end (): Point {
    return this.right.point
  }

  set end (point: Point) {
    this.right.point = point
    this.right.updatePosition()
    this.onMove(this.start, this.end)
  }

  remove () {
    // Delete grip nodes.
    this.left.remove()
    this.right.remove()
    this.link.remove()
  }

  toString (): string {
    let start = positionToString(this.start.pos)
    let end = positionToString(this.end.pos)
    return `(${start}:${end})`
  }
}
