import React, { PropTypes } from 'react';
import Utils from '../../utils';
import { connect } from 'react-redux';
import Section from '../common/Section';
import { InfoBox } from '../common/statelessComponents';

function DisasterStats (props) {
  if (_.isUndefined(props.currentStats)) {
    return (
      <Section title="Current Statistics">
        <p>Nothing to show</p>
      </Section>
    );
  }
  let stats = [];
  for (var key in props.currentStats) {
    if (!props.currentStats.hasOwnProperty(key)) continue;
    stats.push(<InfoBox key={key} copyableClassName="info-copyable" name={key} value={props.currentStats[key]} />)
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
  currentStats: React.PropTypes.object,
  lastStats:  React.PropTypes.object,
};

export default DisasterStats;