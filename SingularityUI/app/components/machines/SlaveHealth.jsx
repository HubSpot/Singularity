import React, { PropTypes } from 'react';
import Utils from '../../utils';
import SlaveHealthMenuItems from './SlaveHealthMenuItems';
import { Dropdown } from 'react-bootstrap';
import { HEALTH_SCALE, WHOLE_NUMBER, STAT_NAMES } from './Constants';

const SlaveHealth = ({slaveInfo, slaveUsage, resource}) => {
  const checkStats = (val, stat) => {
    if (Utils.isResourceStat(stat) && stat !== resource) {
      return null;
    }

    const newStat = {
      name : stat,
      value : (stat === STAT_NAMES.slaveIdStat ? slaveInfo.host : val),
    };

    if (Utils.isResourceStat(stat)) {
      const totalResource = Utils.getMaxAvailableResource(slaveInfo, stat);
      newStat.maybeTotalResource = totalResource;
      newStat.style = {
        backgroundColor : HEALTH_SCALE[Utils.roundTo((val / totalResource) * 100, WHOLE_NUMBER)]
      };
    }

    return newStat;
  };

  const checkedStats = _.map(slaveUsage, checkStats).filter(obj => obj);

  return (
    <Dropdown key={slaveUsage.slaveId} id={slaveUsage.slaveId}>
      <Dropdown.Toggle noCaret={true} className="single-slave-btn" style={checkedStats.find((stat) => stat.style).style} />
      <Dropdown.Menu>
        <SlaveHealthMenuItems stats={checkedStats} />
      </Dropdown.Menu>
    </Dropdown>
  );
};

SlaveHealth.propTypes = {
  slaveUsage : PropTypes.shape({
    slaveId : PropTypes.string.isRequired,
    cpusUsed : PropTypes.number.isRequired,
    memoryBytesUsed : PropTypes.number.isRequired,
    numTasks : PropTypes.number.isRequired,
    timestamp : PropTypes.number.isRequired
  }),
  slaveInfo : PropTypes.shape({
    attributes : PropTypes.object.isRequired,
    resources : PropTypes.object.isRequired
  }),
  resource : PropTypes.string.isRequired
};

export default SlaveHealth;
