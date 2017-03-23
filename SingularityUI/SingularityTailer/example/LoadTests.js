import React, { Component, PropTypes } from 'react';

class LoadTests extends Component {
  componentDidMount() {
    require.ensure([], (require) => {
      require('../test/index.test');
    });
  }

  render() {
    return (
      <div id="mocha" />
    );
  }
}

LoadTests.propTypes = {
  params: PropTypes.object.isRequired
};

export default LoadTests;
