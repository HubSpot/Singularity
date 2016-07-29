import React, { PropTypes } from 'react';
import Utils from '../../utils';

import JSONButton from '../common/JSONButton';
import Section from '../common/Section';
import SimpleTable from '../common/SimpleTable';

function TaskLbUpdates (props) {
  return (
    <Section title="Load Balancer Updates">
      <SimpleTable
        emptyMessage="No Load Balancer Info"
        entries={props.loadBalancerUpdates}
        perPage={5}
        headers={['Timestamp', 'Request Type', 'State', 'Message', '']}
        renderTableRow={(data, index) => {
          return (
            <tr key={index}>
              <td>{Utils.absoluteTimestamp(data.timestamp)}</td>
              <td>{Utils.humanizeText(data.loadBalancerRequestId.requestType)}</td>
              <td>{Utils.humanizeText(data.loadBalancerState)}</td>
              <td>{data.message}</td>
              <td className="actions-column">
                <JSONButton object={data}>{'{ }'}</JSONButton>
              </td>
            </tr>
          );
        }}
      />
    </Section>
  );
}

TaskLbUpdates.propTypes = {
  loadBalancerUpdates: PropTypes.arrayOf(PropTypes.shape({
    loadBalancerRequestId: PropTypes.shape({
      requestType: PropTypes.string
    }).isRequired,
    timestamp: PropTypes.number,
    loadBalancerState: PropTypes.string,
    message: PropTypes.string
  })).isRequired
};

export default TaskLbUpdates;
