CiviCRM Data Integration
========================

This is a Pentaho Data Integration plugin for CiviCRM. It allows you to take advantage
of the power of Pentaho Data Integration tools and use it with your CiviCRM instance.

It uses CiviCRM API version 3, so you don't have to work directly with the data
tables. :-D

We recommend PDI version 4.3 or higher. You can download Pentaho Data Integration here:
http://sourceforge.net/projects/pentaho/

Introduction
------------

You can use the input plugin or the output plugin separately, or both together.
These plugins have been developed to simplify the data manipulation with CiviCRM.

Once plugins have been installed you will be able to get/insert data into your CiviCRM in a easy way.



What do these plugins do?
--------------------------

CIVICRMInput: Gets data from your CiviCRM entity. 
CIVICRMOutput: Inserts / Updates fields into your CiviCRM entity. If you fill in the id entity you will update that record, if not you will insert.

The plugins require some credential information for your CiviCRM instance. You will need your REST URL, Site Key and Api Key from CiviCRM.


How to install it?
------------------

The steps are very simple:

1) Copy the plugin folder into your ${pdi_path}/data-integration/plugins/steps/

2) Copy json-3.1.1.jar lib into your ${pdi_path}/data-integration/libext/

3) Run PDI, and enjoy!! You will find the plugins' steps for making a new transformation in the section input/output


Are you a developer?
--------------------

This section will be available soon.


License
-------

CiviCRM Data Integration. Plugins for managing data (get/insert) into your CiviCRM instance in a easy way. Copyright (C) 2013 Amnesty International (originally developed by Stratebi http://www.stratebi.com/).
This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with this program (see LICENSE.txt). If not, see http://www.gnu.org/licenses/.

