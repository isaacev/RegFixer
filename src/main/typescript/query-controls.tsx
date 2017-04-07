//
// src/main/typescript/query-controls.tsx
// RegEx Frontend
//
// Created on 4/7/17
//

import { PureComponent } from 'react'
import { Button, ButtonColor } from './button'

interface QueryControlsProps {
  totalMatches: number
}

export class QueryControls extends PureComponent<QueryControlsProps, {}> {
  render () {
    let message: string

    if (this.props.totalMatches === Infinity) {
      message = 'Infinity'
    } else if (this.props.totalMatches === 1) {
      message = '1 match'
    } else {
      message = `${this.props.totalMatches} matches`
    }

    return (
      <div className="query-controls">
        <Button name="ask" color={ButtonColor.Blue} glyph="?" />
        <div className="query-status">{message}</div>
      </div>
    )
  }
}
