//
// src/main/typescript/region-list.ts
// RegEx Frontend
//
// Created on 2/20/17
//

import { Position } from 'codemirror'
import { Region } from './region'
import * as util from './util'

export class RegionList {
  // Links to the first and last elements of the list. Both fields are null if
  // the list is empty.
  head: RegionLink
  tail: RegionLink

  // List is initialized empty.
  constructor () {
    this.head = null
    this.tail = null
  }

  // Add a region to the appropriate location within the list.
  insert (reg: Region) {
    // Link is small enough to be at the start of the list. Or the list is
    // empty and this is the first link.
    if (this.head === null || util.lessThanPosition(reg.start, this.head.value.start)) {
      this.head = new RegionLink(this, reg, null, this.head)

      // Set tail pointer if this is the only link.
      if (this.tail === null) {
        this.tail = this.head
      }

      return
    }

    // Link is large enough to be at the end of the list.
    if (util.greaterThanPosition(reg.end, this.tail.value.start)) {
      this.tail.next = new RegionLink(this, reg, this.tail, null)
      this.tail = this.tail.next
      return
    }

    // Link belongs somewhere in the middle of the list.
    let curr = this.head
    while (curr !== null && curr.next !== null) {
      let lowerThreshold = curr.value.right.index
      let upperTheshold = curr.next.value.left.index

      if (lowerThreshold < reg.start && reg.end < upperTheshold) {
        curr.next = new RegionLink(this, reg, curr, curr.next)
        curr.next.next.prev = curr.next
        return
      }

      curr = curr.next
    }

    throw new Error(`${reg.toString()} does not fit in ${this.toString()}`)
  }

  // Loop over each element in the linked list.
  forEach (fn: (reg: Region, index: number) => void) {
    let curr = this.head
    let index = 0

    while (curr !== null) {
      fn(curr.value, index++)
      curr = curr.next
    }
  }

  toString (): string {
    let str = ''

    this.forEach((reg, index) => {
      str += (index > 0) ? ' -> ' : ''
      str += reg.toString()
    })

    return (str === '' ? '(empty)' : str)
  }
}

// RegionLink is a wrapper class that handles linked-list logic around Region
// objects stored in a RegionList linked-list.
export class RegionLink {
  list: RegionList
  value: Region
  prev: RegionLink
  next: RegionLink

  constructor (list: RegionList, value: Region, prev: RegionLink, next: RegionLink) {
    this.list = list
    this.value = value
    this.value.link = this
    this.prev = prev
    this.next = next
  }

  remove () {
    // Remove links to adjacent links.
    if (this.prev !== null) { this.prev.next = this.next }
    if (this.next !== null) { this.next.prev = this.prev }

    // Remove references to this link from the list's head & tail.
    if (this.list.head === this) { this.list.head = this.next }
    if (this.list.tail === this) { this.list.tail = this.prev }
  }
}
