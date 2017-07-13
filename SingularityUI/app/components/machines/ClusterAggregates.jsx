import React, { PropTypes } from 'react';
import CircularProgressbar from 'react-circular-progressbar';
import Utils from '../../utils';
import { STAT_NAMES, HUNDREDTHS_PLACE } from './Constants';

const SlaveAggregates = ({utilization}) => {
  return (
    <div>
      <h3>CPU</h3>
      <div className="row">
        <div className="aggregate col-xs-2">
          <div className="value">
            {utilization.numRequestsWithUnderUtilizedCpu}
          </div>
          <div className="label">
            Requests with Underutilized CPU
          </div>
        </div>
        <div className="aggregate col-xs-2">
          <div className="value">
            {utilization.numRequestsWithOverUtilizedCpu}
          </div>
          <div className="label">
            Requests with Overutilized CPU
          </div>
        </div>

      </div>

      <h3>Memory</h3>
      <div className="row">
        <div className="aggregate col-xs-2">
          <div className="value">
            {utilization.numRequestsWithUnderUtilizedMemBytes}
          </div>
          <div className="label">
            Requests with Underutilized Memory
          </div>
        </div>
      </div>
    </div>
  );
};

SlaveAggregates.propTypes = {
  utilization: PropTypes.object.isRequired
};

export default SlaveAggregates;
