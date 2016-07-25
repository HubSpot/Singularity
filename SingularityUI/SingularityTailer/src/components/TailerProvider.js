import { Component, PropTypes, Children } from 'react';

export default class TailerProvider extends Component {
  getChildContext() {
    return { getTailerState: this.getTailerState };
  }

  constructor(props, context) {
    super(props, context);
    this.getTailerState = props.getTailerState;

    console.log('provider', props);
  }

  componentWillReceiveProps(nextProps) {
    const { getTailerState } = this;
    const { getTailerState: nextGetTailerState } = nextProps;
  }


  render() {
    return Children.only(this.props.children);
  }
}

TailerProvider.propTypes = {
  getTailerState: PropTypes.func.isRequired,
  children: PropTypes.element.isRequired
};

TailerProvider.childContextTypes = {
  getTailerState: PropTypes.func.isRequired
};
