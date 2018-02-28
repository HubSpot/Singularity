import React, { PropTypes } from 'react';
import Utils from '../../utils';
import { connect } from 'react-redux';
import Section from '../common/Section';
import { InfoBox } from '../common/statelessComponents';

function DisasterStats (props) {
  if (_.isUndefined(props.stats) || props.stats.length == 0) {
    return (
      <Section title="Current Statistics">
        <p>Nothing to show</p>
      </Section>
    );
  }

  let stats = [];
  for (var key in props.stats[0]) {
    if (!props.stats[0].hasOwnProperty(key)) continue;
    let value = props.stats[0][key];
    if (key == 'timestamp') {
      value = Utils.timestampFromNow(value);
    }
    stats.push(<InfoBox key={key} name={key} value={value} />)
  }
  return (
    <Section title="Current Statistics">
      <ul className="list-unstyled horizontal-description-list">
        {stats}
      </ul>
    </Section>
  );
}

DisasterStats.propTypes = {
  stats: PropTypes.arrayOf(PropTypes.shape({
    timestamp: PropTypes.number.isRequired,
    numActiveTasks: PropTypes.number.isRequired,
    numPendingTasks: PropTypes.number.isRequired,
    numLateTasks: PropTypes.number.isRequired,
    avgTaskLagMillis: PropTypes.number.isRequired,
    numLostTasks: PropTypes.number.isRequired,
    numActiveSlaves: PropTypes.number.isRequired,
    numLostSlaves: PropTypes.number.isRequired
  }))
};

export default DisasterStats;
