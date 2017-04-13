import React, { Component, PropTypes } from 'react';

const connectToTailer = (Wrapped) => {
  class TailerConnection extends Component {
    constructor(props, context) {
      super(props, context);

      this.getTailerState = context.getTailerState;
    }

    render() {
      return <Wrapped {...this.props} getTailerState={this.getTailerState} />;
    }
  }

  TailerConnection.contextTypes = {
    getTailerState: PropTypes.func
  };

  return TailerConnection;
};

export default connectToTailer;
