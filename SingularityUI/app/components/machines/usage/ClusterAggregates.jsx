import React, { PropTypes } from 'react';
import { Link } from 'react-router';
import Utils from '../../../utils';
import Breakdown from '../../common/Breakdown';

const SlaveAggregates = ({utilization, totalRequests}) => {
  return (
    <div>
      <h3>CPU</h3>
      <div className="row">
        <div className="col-md-2">
          <h4>Requests</h4>
          <Breakdown
            total={totalRequests}
            data={[
              {
                attribute: 'overCpu',
                count: utilization.numRequestsWithOverUtilizedCpu,
                type: 'overdue',
                label: 'Over-utilized',
                link: '/requests',
                percent: (utilization.numRequestsWithOverUtilizedCpu / totalRequests) * 100
              },
              {
                attribute: 'underCpu',
                count: utilization.numRequestsWithUnderUtilizedCpu,
                type: 'cleaning',
                label: 'Under-utilized',
                link: '/requests',
                percent: (utilization.numRequestsWithUnderUtilizedCpu / totalRequests) * 100
              },
              {
                attribute: 'normal',
                count: totalRequests - utilization.numRequestsWithUnderUtilizedCpu - utilization.numRequestsWithOverUtilizedCpu,
                type: 'active',
                label: 'Normal',
                link: '/requests',
                percent: ((totalRequests - utilization.numRequestsWithUnderUtilizedCpu - utilization.numRequestsWithOverUtilizedCpu) / totalRequests) * 100
              }
            ]}
          />
        </div>

        <div className="col-md-10">
          <div className="row">
            <div className="col-md-3">
              <div className="aggregate">
                <div className="value text-danger">
                  {Utils.roundTo(utilization.totalOverUtilizedCpu, 2)}
                </div>
                <div className="label">
                  Total Over-utilized CPU
                </div>
              </div>
            </div>
            <div className="col-md-3">
              <div className="aggregate">
                <div className="value text-danger">
                  {Utils.roundTo(utilization.avgOverUtilizedCpu, 2)}
                </div>
                <div className="label">
                  Average Over-utilized CPU
                </div>
              </div>
            </div>
            <div className="col-md-3">
              <div className="aggregate">
                <div className="value text-danger">
                  {Utils.roundTo(utilization.minOverUtilizedCpu, 2)}
                </div>
                <div className="label">
                  Min Over-utilized CPU
                </div>
              </div>
            </div>
            <div className="col-md-3">
              <div className="aggregate">
                <Link to={`/request/${utilization.maxOverUtilizedCpuRequestId}`}>
                  <div className="value text-danger">
                    {Utils.roundTo(utilization.maxOverUtilizedCpu, 2)}
                  </div>
                  <div className="label">
                    Max Over-utilized CPU
                  </div>
                </Link>
              </div>
            </div>
          </div>
          <div className="row">
            <div className="col-md-3">
              <div className="aggregate">
                <div className="value text-warning">
                  {Utils.roundTo(utilization.totalUnderUtilizedCpu, 2)}
                </div>
                <div className="label">
                  Total Under-utilized CPU
                </div>
              </div>
            </div>
            <div className="col-md-3">
              <div className="aggregate">
                <div className="value text-warning">
                  {Utils.roundTo(utilization.avgUnderUtilizedCpu, 2)}
                </div>
                <div className="label">
                  Average Under-utilized CPU
                </div>
              </div>
            </div>
            <div className="col-md-3">
              <div className="aggregate">
                <div className="value text-warning">
                  {Utils.roundTo(utilization.minUnderUtilizedCpu, 2)}
                </div>
                <div className="label">
                  Min Under-utilized CPU
                </div>
              </div>
            </div>
            <div className="col-md-3">
              <div className="aggregate">
                <Link to={`/request/${utilization.maxUnderUtilizedCpuRequestId}`}>
                  <div className="value text-warning">
                    {Utils.roundTo(utilization.maxUnderUtilizedCpu, 2)}
                  </div>
                  <div className="label">
                    Max Under-utilized CPU
                  </div>
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>

      <h3>Memory</h3>
      <div className="row">
        <div className="col-md-2">
          <h4>Requests</h4>
          <Breakdown
            total={totalRequests}
            data={[
              {
                attribute: 'underMem',
                count: utilization.numRequestsWithUnderUtilizedMemBytes,
                type: 'cleaning',
                label: 'Under-utilized',
                link: '/requests',
                percent: (utilization.numRequestsWithUnderUtilizedMemBytes / totalRequests) * 100
              },
              {
                attribute: 'normal',
                count: totalRequests - utilization.numRequestsWithUnderUtilizedMemBytes,
                type: 'active',
                label: 'Normal',
                link: '/requests',
                percent: ((totalRequests - utilization.numRequestsWithUnderUtilizedMemBytes) / totalRequests) * 100
              }
            ]}
          />
        </div>

        <div className="col-md-10">
          <div className="row">
            <div className="col-md-3">
              <div className="aggregate">
                <div className="value text-warning">
                  {Utils.humanizeFileSize(utilization.totalUnderUtilizedMemBytes)}
                </div>
                <div className="label">
                  Total Under-utilized Memory
                </div>
              </div>
            </div>
            <div className="col-md-3">
              <div className="aggregate">
                <div className="value text-warning">
                  {Utils.humanizeFileSize(utilization.avgUnderUtilizedMemBytes)}
                </div>
                <div className="label">
                  Average Under-utilized Memory
                </div>
              </div>
            </div>
            <div className="col-md-3">
              <div className="aggregate">
                <div className="value text-warning">
                  {Utils.humanizeFileSize(utilization.minUnderUtilizedMemBytes)}
                </div>
                <div className="label">
                  Min Under-utilized Memory
                </div>
              </div>
            </div>
            <div className="col-md-3">
              <div className="aggregate">
                <Link to={`/request/${utilization.maxUnderUtilizedMemBytesRequestId}`}>
                  <div className="value text-warning">
                    {Utils.humanizeFileSize(utilization.maxUnderUtilizedMemBytes)}
                  </div>
                  <div className="label">
                    Max Under-utilized Memory
                  </div>
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

SlaveAggregates.propTypes = {
  utilization: PropTypes.object.isRequired,
  totalRequests: PropTypes.number.isRequired
};

export default SlaveAggregates;
