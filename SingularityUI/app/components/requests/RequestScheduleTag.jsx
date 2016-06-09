import React, { Component } from 'react';
import RequestPropTypes from '../../constants/api/RequestPropTypes';

class RequestScheduleTag extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'RequestScheduleTag';
  }

  render() {
    const request = this.props.request;

    let unifiedSchedule;
    if ('schedule' in request) {
      // CRON
      unifiedSchedule = request.schedule;
    } else if ('quartzSchedule' in request) {
      // QUARTZ
      unifiedSchedule = request.schedule;
    } else {
      return <span />;
    }

    return (
      <div>
        <span className='caps-sublabel'>
          schedule
        </span>
        <span className='schedule-tag'>
          {unifiedSchedule}
        </span>
      </div>
    );
  }
}

RequestScheduleTag.propTypes = {
  request: RequestPropTypes.Request.isRequired
};

export default RequestScheduleTag;
