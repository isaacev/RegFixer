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
  onAccept: () => void
  onReject: () => void
}

export class QueryModal extends PureComponent<QueryModalProps, {}> {
  render () {
    return (
      <div className="query-modal">
        <div className="query-modal-triangle" />
        <QueryModalHeader
          replacement={this.props.replacement}
          onAccept={this.props.onAccept}
          onReject={this.props.onReject} />
      </div>
    )
  }
}

interface QueryModalHeaderProps {
  replacement: string
  onAccept: () => void
  onReject: () => void
}

class QueryModalHeader extends PureComponent<QueryModalHeaderProps, {}> {
  render () {
    return (
      <div className="query-modal-header">
        <div className="replacement">{this.props.replacement}</div>
        <Button color={ButtonColor.Green} glyph="\u2713" onClick={this.props.onAccept} />
        <Button color={ButtonColor.Red} glyph="\u2717" onClick={this.props.onReject} />
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
