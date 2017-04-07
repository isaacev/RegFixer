//
// src/main/typescript/point.ts
// RegEx Frontend
//
// Created on 4/7/17
//

import 'codemirror'

export interface Point {
  index: number
  pos: CodeMirror.Position
  coords: { left: number, right: number, top: number, bottom: number }
}
