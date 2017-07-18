import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import CollapsableSection from '../common/CollapsableSection';
import { UsageInfo } from '../common/statelessComponents';
import Utils from '../../utils';
import BootstrapTable from 'react-bootstrap/lib/Table';
import Loader from "../common/Loader";

export const HUNDREDTHS_PLACE = 2;

const RequestUtilization = ({isFetching, utilization}) => {
  const isCpuOverAllocated = utilization &&
    (utilization.maxCpuUsed > utilization.cpuReserved / utilization.numTasks);
  const attributes = utilization && (
      <div className="row">
        <div className="col-md-3">
          <UsageInfo
            title="CPU per task average"
            total={utilization.cpuReserved / utilization.numTasks}
            used={utilization.cpuUsed / utilization.numTasks}
            style={utilization.cpuUsed >= utilization.cpuReserved ? 'danger' : null}
          >
            <p>{Utils.roundTo(utilization.cpuUsed / utilization.numTasks, HUNDREDTHS_PLACE)} of {utilization.cpuReserved / utilization.numTasks} CPU reserved</p>
            <BootstrapTable responsive={false} striped={true} style={{marginTop: '10px'}}>
              <tbody>
                <tr>
                  <td>Min CPU (all tasks)</td>
                  <td>{Utils.roundTo(utilization.minCpuUsed, HUNDREDTHS_PLACE)}</td>
                </tr>
                <tr>
                  <td className={isCpuOverAllocated ? 'danger' : ''}>Max CPU (all tasks)</td>
                  <td className={isCpuOverAllocated ? 'danger' : ''}>{Utils.roundTo(utilization.maxCpuUsed, HUNDREDTHS_PLACE)}</td>
                </tr>
              </tbody>
            </BootstrapTable>
          </UsageInfo>
        </div>
        <div className="col-md-3">
          <UsageInfo
            title="Memory per task average"
            total={utilization.memBytesReserved / utilization.numTasks}
            used={utilization.memBytesUsed / utilization.numTasks}
            style={utilization.memBytesUsed >= utilization.memBytesReserved ? 'danger' : null}
          >
            <p>{Utils.humanizeFileSize(utilization.memBytesUsed / utilization.numTasks)} of {Utils.humanizeFileSize(utilization.memBytesReserved / utilization.numTasks)} reserved</p>
            <BootstrapTable responsive={false} striped={true} style={{marginTop: '10px'}}>
              <tbody>
                <tr>
                  <td>Min memory (all tasks)</td>
                  <td>{Utils.humanizeFileSize(utilization.minMemBytesUsed)}</td>
                </tr>
                <tr>
                  <td>Max memory (all tasks)</td>
                  <td>{Utils.humanizeFileSize(utilization.maxMemBytesUsed)}</td>
                </tr>
              </tbody>
            </BootstrapTable>
          </UsageInfo>
        </div>
    </div>
  );

  return utilization ? (
    <CollapsableSection id="request-utilization" title="Resource usage" subtitle="(past 24 hours)" defaultExpanded={isCpuOverAllocated}>
      {isFetching ? <Loader /> : attributes}
    </CollapsableSection>
  ) : <div></div>;
};

RequestUtilization.propTypes = {
  requestId: PropTypes.string.isRequired,
  isFetching: PropTypes.bool.isRequired,
  utilization: PropTypes.object
};


const mapStateToProps = function(state, ownProps) {
  const requestId = ownProps.requestId;
  return {
    isFetching: state.api.utilization.isFetching,
    utilization: _.find(state.api.utilization.data.requestUtilizations, (request) => {
      return request.requestId === requestId;
    })
  };
};

export default connect(
  mapStateToProps
)(RequestUtilization);
