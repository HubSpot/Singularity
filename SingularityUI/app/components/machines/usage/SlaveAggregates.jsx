import React, { PropTypes } from 'react';
import Utils from '../../../utils';
import { STAT_NAMES } from '../Constants';
import LabeledColumn from './LabeledColumn';
import Aggregate from './Aggregate';

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
      <Aggregate width={2} value={slaves.length} label="Active Slaves" vcenter={true} />
      <Aggregate width={2} value={activeTasks} label="Tasks Running" vcenter={true} />

      <LabeledColumn title="Current" width={4}>
        <Aggregate width={2} value={getCpuUtilizationPct(slaves, slaveUsages)} graph={true} label="CPU" />
        <Aggregate width={2} value={getMemUtilizationPct(slaves, slaveUsages)} graph={true} label="Memory" />
      </LabeledColumn>

      <LabeledColumn className="info" title="24-Hour Average" width={4}>
        <Aggregate width={2} value={Utils.toDisplayPercentage(utilization.totalCpuUsed, utilization.totalCpuAvailable)} graph={true} label="CPU" />
        <Aggregate width={2} value={Utils.toDisplayPercentage(utilization.totalMemBytesUsed, utilization.totalMemBytesAvailable)} graph={true} label="Memory" />
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
