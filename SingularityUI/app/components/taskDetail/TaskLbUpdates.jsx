import React, { PropTypes } from 'react';
import Utils from '../../utils';

import JSONButton from '../common/JSONButton';
import Section from '../common/Section';
import UITable from '../common/table/UITable';
import Column from '../common/table/Column';

function TaskLbUpdates (props) {
  return (
    <Section title="Load Balancer Updates">
      <UITable
        emptyTableMessage="This task has no history yet"
        data={props.loadBalancerUpdates}
        keyGetter={(loadBalancerUpdate) => loadBalancerUpdate.timestamp}
        rowChunkSize={5}
        paginated={true}
      >
        <Column
          label="Timestamp"
          id="timestamp"
          key="timestamp"
          cellData={(loadBalancerUpdate) => Utils.timestampFromNow(loadBalancerUpdate.timestamp)}
        />
        <Column
          label="Request Type"
          id="request-type"
          key="request-type"
          cellData={(loadBalancerUpdate) => Utils.humanizeText(loadBalancerUpdate.loadBalancerRequestId.requestType)}
        />
        <Column
          label="State"
          id="state"
          key="state"
          cellData={(loadBalancerUpdate) => Utils.humanizeText(loadBalancerUpdate.loadBalancerState)}
        />
        <Column
          label="Message"
          id="message"
          key="message"
          cellData={(loadBalancerUpdate) => loadBalancerUpdate.message}
        />
        <Column
          id="actions-column"
          key="actions-column"
          className="actions-column"
          cellData={(loadBalancerUpdate) => <JSONButton object={loadBalancerUpdate} showOverlay={true}>{'{ }'}</JSONButton>}
        />
      </UITable>
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
