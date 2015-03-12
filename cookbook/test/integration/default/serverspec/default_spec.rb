# Encoding: utf-8
require 'spec_helper'

describe 'Filesystem' do
  describe file('/usr/local/singularity') do
    it { should be_directory }
    it { should be_owned_by 'singularity' }
  end
end

describe 'Services' do
  # 2181 - Zookeeper
  # 3306 - MySQL
  # 5050 - Mesos Master
  # 5051 - Mesos Slave
  # 7099 - Singularity
  [3306, 5050, 7099].each do |p|
    describe port(p) do
      it { should be_listening.with('tcp') }
    end
  end
  describe port(2181) do
    it { should be_listening.with('tcp6') }
  end
  describe file('/etc/mesos-slave/attributes/rackid') do
    it { is_expected.to be_file }
    it { is_expected.to contain(/^us-east-1b$/) }
  end
end
