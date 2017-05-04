//
// src/main/typescript/popover-zone.ts
// RegEx Frontend
//
// Created on 5/1/17
//

export class PopoverZone {
  private x: number
  private y: number
  private width: number
  private height: number
  private onOver: () => void
  private onOut: () => void

  constructor (x: number, y: number, width: number, height: number) {
    this.x = x
    this.y = y
    this.width = width
    this.height = height
    this.onOver = () => {}
    this.onOut = () => {}
  }

  equals (other: PopoverZone): boolean {
    return (other !== null) &&
           (this.x === other.x) &&
           (this.y === other.y) &&
           (this.width === other.width) &&
           (this.height === other.height)
  }

  contains (x: number, y: number): boolean {
    let fitsHoriz = (this.x <= x && x <= (this.x + this.width))
    let fitsVert  = (this.y <= y && y <= (this.y + this.height))

    return (fitsHoriz && fitsVert)
  }

  on (event: 'over' | 'out', cb: () => void) {
    if (event === 'over') {
      this.onOver = cb
    } else if (event === 'out') {
      this.onOut = cb
    }
  }

  over () {
    this.onOver.apply(this.onOver)
  }

  out () {
    this.onOut.apply(this.onOut)
  }
}
