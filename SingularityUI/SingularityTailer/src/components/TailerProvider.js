import { Component, PropTypes, Children } from 'react';

export default class TailerProvider extends Component {
  constructor(props, context) {
    super(props, context);
    this.getTailerState = props.getTailerState;
  }

  getChildContext() {
    return { getTailerState: this.getTailerState };
  }

  render() {
    return Children.only(this.props.children);
  }
}

TailerProvider.propTypes = {
  getTailerState: PropTypes.func.isRequired,
  children: PropTypes.element
};

TailerProvider.childContextTypes = {
  getTailerState: PropTypes.func.isRequired
};
