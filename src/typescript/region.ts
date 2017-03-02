//
// src/typescript/region.ts
// RegEx Frontend
//
// Created on 2/20/17
//

import { Position } from 'codemirror'
import { CorpusEditor } from './corpus-editor'
import { RegionLink } from './region-list'
import { LeftGrip, RightGrip } from './grip'
import * as util from './util'

function positionToString (pt: Position): string {
  return `(${pt.line}:${pt.ch})`
}

export class Region {
  editor: CorpusEditor
  left: LeftGrip
  right: RightGrip
  link: RegionLink
  color: string

  onMove: (left: Position, right: Position) => void = util.noop

  constructor (editor: CorpusEditor, start: Position, end: Position, color: string) {
    this.editor = editor
    this.left = new LeftGrip(this, start, color)
    this.right = new RightGrip(this, end, color)
    this.color = color
  }

  get start (): Position {
    return this.left.index
  }

  set start (index: Position) {
    this.left.index = index
    this.left.updatePosition()
    this.onMove(this.start, this.end)
  }

  get end (): Position {
    return this.right.index
  }

  set end (index: Position) {
    this.right.index = index
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
    let start = positionToString(this.start)
    let end = positionToString(this.end)
    return `(${start}:${end})`
  }
}
