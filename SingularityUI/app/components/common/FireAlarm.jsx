import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { Alert } from 'react-bootstrap';

class FireAlarm extends Component {
  constructor(props) {
    super(props);
  }

  render() {
    const { fireAlarm } = this.props;

    if (fireAlarm) {
      return (
        <Alert bsStyle="warning">
          <div className="fireAlarm">
            <span className="h4 fireAlarmTitle">{fireAlarm.title}</span>
            <span className="fireAlarmMessage">{fireAlarm.message}</span>
            <a className="fireAlarmUrl">{fireAlarm.url}</a>
          </div>
        </Alert>
      );
    }

    return null;
  }
}

FireAlarm.propTypes = {
  fireAlarm: PropTypes.object,
};

const mapStateToProps = (state) => {
  return {
    fireAlarm: state.api.status.data.fireAlarm,
  };
};

export default connect(mapStateToProps)(FireAlarm);
