import React from 'react';

import SidebarFilter from '../common/SidebarFilter';
import SearchBar from '../common/SearchBar';
import TabBar from '../common/TabBar';

const RequestsPage = () => (
  <div>
    <SidebarFilter />
    <div>
      <SearchBar />
      <TabBar />
    </div>
  </div>
);

export default RequestsPage;
