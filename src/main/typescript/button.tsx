//
// src/main/typescript/button.tsx
// RegEx Frontend
//
// Created on 4/7/17
//

import { PureComponent } from 'react'

export enum ButtonColor {
  Red,
  Green,
  Blue,
}

interface ButtonProps {
  name: string
  color: ButtonColor
  glyph: string
}

export class Button extends PureComponent<ButtonProps, {}> {
  render () {
    return (
      <button
        className="action"
        data-color={ButtonColor[this.props.color].toLowerCase()}
        name={this.props.name}>{this.props.glyph}</button>
    )
  }
}
