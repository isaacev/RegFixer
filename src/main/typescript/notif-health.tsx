import { PureComponent } from 'react'

interface Props {
  health: 'green' | 'yellow' | 'red'
}

export class NotifHealth extends PureComponent<Props, {}> {
  render () {
    return (
      <div className={`health health-${this.props.health}`} />
    )
  }
}
