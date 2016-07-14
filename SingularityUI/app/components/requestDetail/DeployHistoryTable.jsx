import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import Section from '../common/Section';

import Utils from '../../utils';

import { FetchDeploysForRequest } from '../../actions/api/history';

import ServerSideTable from '../common/ServerSideTable';
import JSONButton from '../common/JSONButton';

const DeployHistoryTable = ({requestId, deploysAPI}) => {
  const deploys = deploysAPI ? deploysAPI.data : [];
  const emptyTableMessage = (Utils.api.isFirstLoad(deploysAPI)
    ? 'Loading...'
    : 'No deploys'
  );
  return (
    <Section id="deploy-history" title="Deploy history">
      <ServerSideTable
        emptyMessage={emptyTableMessage}
        entries={deploys}
        paginate={deploys.length >= 5}
        perPage={5}
        fetchAction={FetchDeploysForRequest}
        fetchParams={[requestId]}
        headers={['Deploy ID', 'Status', 'User', 'Timestamp', '']}
        renderTableRow={(data, index) => {
          const { deployMarker, deployResult } = data;
          return (
            <tr key={index}>
              <td>
                <a href={`${config.appRoot}/request/${deployMarker.requestId}/deploy/${deployMarker.deployId}`}>
                  {deployMarker.deployId}
                </a>
              </td>
              <td>
                {deployResult ? Utils.humanizeText(deployResult.deployState) : 'Pending'}
              </td>
              <td>
                {deployMarker.user.split('@')[0]}
              </td>
              <td>
                {Utils.timestampFromNow(deployMarker.timestamp)}
              </td>
              <td className="actions-column">
                <JSONButton object={data}>{'{ }'}</JSONButton>
              </td>
            </tr>
          );
        }}
      />
    </Section>
  );
};

DeployHistoryTable.propTypes = {
  requestId: PropTypes.string.isRequired,
  deploysAPI: PropTypes.object.isRequired
};

const mapStateToProps = (state, ownProps) => ({
  deploysAPI: Utils.maybe(
    state.api.deploysForRequest,
    [ownProps.requestId]
  )
});

export default connect(
  mapStateToProps,
  null
)(DeployHistoryTable);
