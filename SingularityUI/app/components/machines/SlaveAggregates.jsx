import React, { PropTypes } from 'react';
import CircularProgressbar from 'react-circular-progressbar';

const SlaveAggregates = ({totalSlaves, totalTasks, avgCpu, avgMemory}) => {
  return (
    <div className="slave-aggregates row">
      <div className="total-slaves col-xs-3">
        <div id="value">
          {totalSlaves}
        </div>
        <div id="label">
          Total Slaves
        </div>
      </div>
      <div className="total-tasks col-xs-3">
        <div id="value">
          {totalTasks}
        </div>
        <div id="label">
          Tasks Running
        </div>
      </div>
      <div className="avg-cpu col-xs-3">
        <CircularProgressbar percentage={avgCpu} initialAnimation={true} textForPercentage={(pct) => `${pct}%`} />
      </div>
      <div className="avg-memory col-xs-3">
        <CircularProgressbar percentage={avgMemory} initialAnimation={true} textForPercentage={(pct) => `${pct}%`} />
      </div>
    </div>
  );
};

SlaveAggregates.propTypes = {
  totalSlaves : PropTypes.number.isRequired,
  totalTasks : PropTypes.number.isRequired,
  avgCpu : PropTypes.number.isRequired,
  avgMemory : PropTypes.number.isRequired
};

export default SlaveAggregates;
