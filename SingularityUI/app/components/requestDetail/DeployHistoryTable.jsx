import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import Utils from '../../utils';

import { FetchDeploysForRequest } from '../../actions/api/history';

import ServerSideTable from '../common/ServerSideTable';
import JSONButton from '../common/JSONButton';

const DeployHistoryTable = ({requestId, deploys}) => {
  return (
    <div>
      <h2>Deploy history</h2>
      <ServerSideTable
        emptyMessage="No deploys"
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
    </div>
  );
};

DeployHistoryTable.propTypes = {
  requestId: PropTypes.string.isRequired,
  deploys: PropTypes.arrayOf(PropTypes.object).isRequired
};

const mapStateToProps = (state, ownProps) => ({
  deploys: Utils.maybe(state.api.deploysForRequest, [ownProps.requestId, 'data'])
});

export default connect(
  mapStateToProps,
  null
)(DeployHistoryTable);
