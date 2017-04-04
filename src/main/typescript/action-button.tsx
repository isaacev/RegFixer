import { PureComponent } from 'react'

export enum ActionButtonColor {
  Red,
  Green,
  Blue,
}

interface ActionButtonProps {
  name: string
  color: ActionButtonColor
  glyph: string
}

export default class ActionButton extends PureComponent<ActionButtonProps, {}> {
  render () {
    let colorClass = ActionButtonColor[this.props.color].toLowerCase()

    return (
      <button className={`action ${colorClass}`} name={this.props.name}>{this.props.glyph}</button>
    )
  }
}
