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
  color: ButtonColor
  glyph: string
  onClick: () => void
}

export class Button extends PureComponent<ButtonProps, {}> {
  render () {
    return (
      <button
        className="action"
        data-color={ButtonColor[this.props.color].toLowerCase()}
        onClick={this.props.onClick}>{this.props.glyph}</button>
    )
  }
}
