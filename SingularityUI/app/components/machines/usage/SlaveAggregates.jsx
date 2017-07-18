import React, { PropTypes } from 'react';
import CircularProgressbar from 'react-circular-progressbar';
import Utils from '../../../utils';
import { STAT_NAMES } from '../Constants';
import LabeledColumn from './LabeledColumn';

const getPctSlaveUsage = (usageMapper, resourceMapper) => (slaves, slaveUsages) => {
  const totalUsage = slaveUsages.map(usageMapper)
    .reduce((acc, val) => acc + parseFloat(val), 0);

  const totalResource = slaves.map(resourceMapper)
    .reduce((acc, val) => acc + parseFloat(val), 0);

  return Utils.toDisplayPercentage(totalUsage, totalResource);
};

const getCpuUtilizationPct = getPctSlaveUsage(
  usage => usage.cpusUsed,
  slave => Utils.getMaxAvailableResource(slave, STAT_NAMES.cpusUsedStat)
);

const getMemUtilizationPct = getPctSlaveUsage(
  usage => usage.memoryBytesUsed,
  slave => Utils.getMaxAvailableResource(slave, STAT_NAMES.memoryBytesUsedStat)
);

const SlaveAggregates = ({slaves, slaveUsages, activeTasks, utilization}) => {
  return (
    <div className="slave-aggregates row">
      <div className="aggregate vcenter col-xs-2">
        <div className="value">
          {slaves.length}
        </div>
        <div className="label">
          Active Slaves
        </div>
      </div>
      <div className="aggregate vcenter col-xs-2">
        <div className="value">
          {activeTasks}
        </div>
        <div className="label">
          Tasks Running
        </div>
      </div>

      <LabeledColumn title="Current" width={4}>
        <div className="aggregate graph col-xs-2">
          <CircularProgressbar percentage={getCpuUtilizationPct(slaves, slaveUsages)} initialAnimation={true} textForPercentage={(pct) => `${pct}%`} />
          <div className="label">
            CPU
          </div>
        </div>
        <div className="aggregate graph col-xs-2">
          <CircularProgressbar percentage={getMemUtilizationPct(slaves, slaveUsages)} initialAnimation={true} textForPercentage={(pct) => `${pct}%`} />
          <div className="label">
            Memory
          </div>
        </div>
      </LabeledColumn>

      <LabeledColumn className="info" title="24-Hour Average" width={4}>
        <div className="aggregate graph col-xs-2">
          <CircularProgressbar percentage={Utils.toDisplayPercentage(utilization.totalCpuUsed, utilization.totalCpuAvailable)} initialAnimation={true} textForPercentage={(pct) => `${pct}%`} />
          <div className="label">
            CPU
          </div>
        </div>
        <div className="aggregate graph col-xs-2">
          <CircularProgressbar percentage={Utils.toDisplayPercentage(utilization.totalMemBytesUsed, utilization.totalMemBytesAvailable)} initialAnimation={true} textForPercentage={(pct) => `${pct}%`} />
          <div className="label">
            Memory
          </div>
        </div>
      </LabeledColumn>
    </div>
  );
};

SlaveAggregates.propTypes = {
  slaves: PropTypes.array,
  slaveUsages: PropTypes.array,
  activeTasks: PropTypes.number.isRequired,
  utilization: PropTypes.object
};

export default SlaveAggregates;
