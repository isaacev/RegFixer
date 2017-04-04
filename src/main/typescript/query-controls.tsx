import { PureComponent } from 'react'
import ActionButton, { ActionButtonColor } from './action-button'
import StatusBubble, { BubbleStatus } from './status-bubble'

interface QueryControlsProps {
  totalMatches: number
  status: BubbleStatus
}

export default class QueryControls extends PureComponent<QueryControlsProps, {}> {
  render () {
    return (
      <div className="query-controls">
        <ActionButton name="undo" color={ActionButtonColor.Blue} glyph="\uF6D7" />
        <ActionButton name="redo" color={ActionButtonColor.Blue} glyph="\uF6D8" />
        <StatusBubble status={this.props.status} totalMatches={this.props.totalMatches} />
      </div>
    )
  }
}
