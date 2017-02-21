import React, { PropTypes } from 'react';
import CircularProgressbar from 'react-circular-progressbar';
import Utils from '../../utils';
import { STAT_NAMES, HUNDREDTHS_PLACE } from './Constants';

const getAvgSlaveUsage = (slaves, slaveUsages, usageCallback, resourceCallback) => {
  const totalUsage = slaveUsages.map(usageCallback)
                                .reduce((acc, val) => acc + parseFloat(val), 0);

  const totalResource = slaves.map(resourceCallback)
                              .reduce((acc, val) => acc + parseFloat(val), 0);

  return Utils.roundTo((totalUsage / totalResource), HUNDREDTHS_PLACE);
};

const getAvgCpu = (slaves, slaveUsages) => {
  return getAvgSlaveUsage(slaves,
                          slaveUsages,
                          usage => usage.cpusUsed,
                          slave => Utils.getMaxAvailableResource(slave, STAT_NAMES.cpusUsedStat));
};

const getAvgMem = (slaves, slaveUsages) => {
  return getAvgSlaveUsage(slaves,
                          slaveUsages,
                          usage => usage.memoryBytesUsed,
                          slave => Utils.getMaxAvailableResource(slave, STAT_NAMES.memoryBytesUsedStat) * Math.pow(1024, 2));
};

const SlaveAggregates = ({slaves, slaveUsages}) => {
  return (
    <div className="slave-aggregates row">
      <div className="total-slaves col-xs-3">
        <div id="value">
          {slaves.length}
        </div>
        <div id="label">
          Total Slaves
        </div>
      </div>
      <div className="total-tasks col-xs-3">
        <div id="value">
          {"todo"}
        </div>
        <div id="label">
          Tasks Running
        </div>
      </div>
      <div className="avg-cpu col-xs-3">
        <CircularProgressbar percentage={getAvgCpu(slaves, slaveUsages)} initialAnimation={true} textForPercentage={(pct) => `${pct}%`} />
        <div id="label">
          Cpu
        </div>
      </div>
      <div className="avg-memory col-xs-3">
        <CircularProgressbar percentage={getAvgMem(slaves, slaveUsages)} initialAnimation={true} textForPercentage={(pct) => `${pct}%`} />
        <div id="label">
          Memory
        </div>
      </div>
    </div>
  );
};

SlaveAggregates.propTypes = {
  slaves : PropTypes.array,
  slaveUsages : PropTypes.array
};

export default SlaveAggregates;
