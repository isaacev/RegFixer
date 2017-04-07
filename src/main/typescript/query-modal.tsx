//
// src/main/typescript/query-modal.tsx
// RegEx Frontend
//
// Created on 4/7/17
//

import { PureComponent } from 'react'
import { Button, ButtonColor } from './button'

interface QueryModalProps {
  replacement: string
}

export class QueryModal extends PureComponent<QueryModalProps, {}> {
  render () {
    return (
      <div className="query-modal">
        <div className="query-modal-triangle" />
        <QueryModalHeader replacement={this.props.replacement} />
        <QueryModalExplanation />
      </div>
    )
  }
}

interface QueryModalHeaderProps {
  replacement: string
}

class QueryModalHeader extends PureComponent<QueryModalHeaderProps, {}> {
  render () {
    return (
      <div className="query-modal-header">
        <div className="replacement">{this.props.replacement}</div>
        <Button name="accept" color={ButtonColor.Green} glyph="\u2713" />
        <Button name="reject" color={ButtonColor.Red} glyph="\u2717" />
      </div>
    )
  }
}

class QueryModalExplanation extends PureComponent<{}, {}> {
  render () {
    return (
      <div className="query-modal-explanation">
        <h2>Explanation</h2>
        <p>Lorem ipsum dolor sit amet.</p>
      </div>
    )
  }
}
