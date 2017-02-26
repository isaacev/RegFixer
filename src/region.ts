//
// src/region.ts
// RegEx Frontend
//
// Created on 2/20/17
//

import { CorpusEditor } from './corpus-editor'
import { RegionLink } from './region-list'
import { LeftGrip, RightGrip } from './grip'
import { Point }  from './point'

function pointToString (pt: Point): string {
  return `(${pt.line}:${pt.ch})`
}

export class Region {
  editor: CorpusEditor
  left: LeftGrip
  right: RightGrip
  link: RegionLink
  color: string

  constructor (editor: CorpusEditor, start: Point, end: Point, color: string) {
    this.editor = editor
    this.left = new LeftGrip(this, start, color)
    this.right = new RightGrip(this, end, color)
    this.color = color
  }

  get start (): Point {
    return this.left.index
  }

  set start (index: Point) {
    this.left.index = index
    this.left.updatePosition()
    this.editor.drawCanvas()
  }

  get end (): Point {
    return this.right.index
  }

  set end (index: Point) {
    this.right.index = index
    this.right.updatePosition()
    this.editor.drawCanvas()
  }

  remove () {
    // Delete grip nodes.
    this.left.remove()
    this.right.remove()
    this.link.remove()
  }

  toString (): string {
    let start = pointToString(this.start)
    let end = pointToString(this.end)
    return `(${start}:${end})`
  }
}
