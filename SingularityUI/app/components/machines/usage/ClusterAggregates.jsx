import React, { PropTypes } from 'react';
import CircularProgressbar from 'react-circular-progressbar';
import Utils from '../../../utils';
import { STAT_NAMES, HUNDREDTHS_PLACE } from '../Constants';

const SlaveAggregates = ({utilization}) => {
  return (
    <div>
      <h3>CPU</h3>
      <div className="story row">
        <div className="col-xs-12">
          <span className="number">{utilization.numRequestsWithUnderUtilizedCpu}</span>
          <span>requests are under-utilizing</span>
          <span className="number">{Utils.roundTo(utilization.totalUnderUtilizedCpu, 2)}</span>
          <span>CPUs with an average of</span>
          <span className="number">{Utils.roundTo(utilization.avgUnderUtilizedCpu, 2)}</span>
          <span>CPUs each.</span>
        </div>
      </div>

      <div className="story row">
        <div className="col-xs-12">
          <span className="number">{utilization.numRequestsWithOverUtilizedCpu}</span>
          <span>requests are over-utilizing</span>
          <span className="number">{Utils.roundTo(utilization.totalOverUtilizedCpu, 2)}</span>
          <span>CPUs with an average of</span>
          <span className="number">{Utils.roundTo(utilization.avgOverUtilizedCpu, 2)}</span>
          <span>CPUs each.</span>
        </div>
      </div>

      <h3>Memory</h3>
      <div className="story row">
        <div className="col-xs-12">
          <span className="number">{utilization.numRequestsWithUnderUtilizedMemBytes}</span>
          <span>requests are under-utilizing</span>
          <span className="number">{Utils.humanizeFileSize(utilization.totalUnderUtilizedMemBytes)}</span>
          <span>of memory with an average of</span>
          <span className="number">{Utils.humanizeFileSize(utilization.avgUnderUtilizedMemBytes)}</span>
          <span>each.</span>
        </div>
      </div>
    </div>
  );
};

SlaveAggregates.propTypes = {
  utilization: PropTypes.object.isRequired
};

export default SlaveAggregates;
