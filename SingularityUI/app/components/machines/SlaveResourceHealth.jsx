import React, { PropTypes } from 'react';
import Utils from '../../utils';
import SlaveResourceHealthMenuItems from './SlaveResourceHealthMenuItems';
import { Dropdown } from 'react-bootstrap';
import { HEALTH_SCALE, STAT_NAMES } from './Constants';

const SlaveResourceHealth = ({slaveInfo, slaveUsage, resource, totalResource, utilization}) => {
  const checkStats = (val, stat) => {
    if (Utils.isResourceStat(stat) && stat !== resource) {
      return null;
    }

    const newStat = {
      name : stat,
      value : (stat === STAT_NAMES.slaveIdStat ? slaveInfo.host : val),
    };

    if (Utils.isResourceStat(stat)) {
      newStat.maybeTotalResource = totalResource;
    }

    return newStat;
  };

  const checkedStats = _.map(slaveUsage, checkStats).filter(obj => obj);

  return (
    <Dropdown key={slaveUsage.slaveId} id={slaveUsage.slaveId}>
      <Dropdown.Toggle noCaret={true} className="single-slave-btn" style={{backgroundColor : HEALTH_SCALE[utilization]}} />
      <Dropdown.Menu>
        <SlaveResourceHealthMenuItems stats={checkedStats} />
      </Dropdown.Menu>
    </Dropdown>
  );
};

SlaveResourceHealth.propTypes = {
  slaveUsage : PropTypes.shape({
    slaveId : PropTypes.string.isRequired,
    cpusUsed : PropTypes.number.isRequired,
    memoryBytesUsed : PropTypes.number.isRequired,
    numTasks : PropTypes.number.isRequired,
    timestamp : PropTypes.number.isRequired
  }),
  slaveInfo : PropTypes.shape({
    host : PropTypes.string.isRequired,
    attributes : PropTypes.object.isRequired,
    resources : PropTypes.object.isRequired
  }),
  resource : PropTypes.string.isRequired,
  totalResource : PropTypes.number.isRequired,
  utilization : PropTypes.number.isRequired
};

export default SlaveResourceHealth;
