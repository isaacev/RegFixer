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
  onAsk: () => void
}

export class QueryControls extends PureComponent<QueryControlsProps, {}> {
  render () {
    let message: string

    if (this.props.totalMatches === Infinity) {
      message = '\u221E matches'
    } else if (this.props.totalMatches === 1) {
      message = '1 match'
    } else {
      message = `${this.props.totalMatches} matches`
    }

    return (
      <div className="query-controls">
        <Button onClick={this.props.onAsk} color={ButtonColor.Blue} glyph="?" />
        <div className="query-status">{message}</div>
      </div>
    )
  }
}
