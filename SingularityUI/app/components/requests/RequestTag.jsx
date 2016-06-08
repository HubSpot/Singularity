import React, { Component, PropTypes } from 'react';

import classNames from 'classnames';

class RequestTag extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'RequestTag';
  }

  render() {
    const classes = classNames({
      // Type tags
      'service-tag': this.props.of === 'SERVICE',
      'worker-tag': this.props.of === 'WORKER',
      'scheduled-tag': this.props.of === 'SCHEDULED',
      'onDemand-tag': this.props.of === 'ON_DEMAND',
      'runOnce-tag': this.props.of === 'RUN_ONCE',

      // State tags
      'active-tag': this.props.of === 'ACTIVE'
    });

    const text = {
      'SERVICE': 'service',
      'WORKER': 'worker',
      'SCHEDULED': 'scheduled',
      'ON_DEMAND': 'on demand',
      'RUN_ONCE': 'run once',
    }[this.props.of] || this.props.of;


    return (
      <span className={classes}>{text}</span>
    );
  }
}

RequestTag.propTypes = {
  of: PropTypes.string.isRequired
}

export default RequestTag;
