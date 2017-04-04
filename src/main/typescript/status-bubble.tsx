import { PureComponent } from 'react'

export enum BubbleStatus {
  Normal,
  Infinite,
  Error,
}

interface StatusBubbleProps {
  status: BubbleStatus
  totalMatches: number
}

export default class StatusBubble extends PureComponent<StatusBubbleProps, {}> {
  render () {
    let message = ''

    if (this.props.status == BubbleStatus.Normal) {
      if (this.props.totalMatches === 1) {
        message = `${this.props.totalMatches} match`
      } else {
        message = message = `${this.props.totalMatches} matches`
      }
    } else if (this.props.status == BubbleStatus.Infinite) {
      message = 'Infinite'
    }

    return (
      <span className="bubble" data-status={this.props.status}>
        {message}
      </span>
    )
  }
}
