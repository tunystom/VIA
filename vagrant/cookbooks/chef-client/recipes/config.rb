#
# Author:: Joshua Timberman (<joshua@opscode.com>)
# Author:: Joshua Sierles (<joshua@37signals.com>)
# Author:: Seth Chisamore (<schisamo@opscode.com>)
# Cookbook Name:: chef
# Recipe:: client
#
# Copyright 2008-2011, Opscode, Inc
# Copyright 2009, 37signals
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

root_group = value_for_platform(
  ["openbsd", "freebsd", "mac_os_x", "mac_os_x_server"] => { "default" => "wheel" },
  ["windows"] => { "default" => "Administrators" },
  "default" => "root"
)

chef_node_name = Chef::Config[:node_name] == node["fqdn"] ? false : Chef::Config[:node_name]
log_path = case node["chef_client"]["log_file"]
  when String
    "'#{File.join(node["chef_client"]["log_dir"], node["chef_client"]["log_file"])}'"
  else
    'STDOUT'
  end


%w{run_path cache_path backup_path log_dir conf_dir}.each do |key|
  directory node['chef_client'][key] do
    recursive true
    if node.recipe?("chef-server")
      owner "chef"
      group "chef"
    else
      owner "root"
      group root_group
    end
    mode 0755
  end
end

chef_requires = []
node["chef_client"]["load_gems"].each do |gem_name, gem_info_hash|
  gem_info_hash ||= {}
  chef_gem gem_name do
    action gem_info_hash[:action] || :install
    version gem_info_hash[:version] if gem_info_hash[:version]
  end
  chef_requires.push(gem_info_hash[:require_name] || gem_name)
end

template "#{node["chef_client"]["conf_dir"]}/client.rb" do
  source "client.rb.erb"
  owner "root"
  group root_group
  mode 0644
  variables(
    :chef_node_name => chef_node_name,
    :chef_log_location => log_path,
    :chef_log_level => node["chef_client"]["log_level"] || :info,
    :chef_environment => node["chef_client"]["environment"],
    :chef_requires => chef_requires,
    :chef_verbose_logging => node["chef_client"]["verbose_logging"]
  )
  notifies :create, "ruby_block[reload_client_config]"
end

ruby_block "reload_client_config" do
  block do
    Chef::Config.from_file("#{node["chef_client"]["conf_dir"]}/client.rb")
  end
  action :nothing
end
