# Open Archive "Sav Space" Collection Specification

Nathan Freitas aka @n8fr8
Guardian Project
Created: 21 November 2018
Updated: 26 November 2018

## Summary

This document is for outlining the folder and file structure for the publishing of an Open Archive (OA) collection, otherwise known as a "Sav Space". This is specifies how to serialize to a disk (or a tar/zip archive file) the set of media files and metadata imported, organized and curated in the Open Archive app. It will also specify the format of the metadata included, and what extra data can also be included, such as OpenPGP signatures.

## Collections

The current development work on the OA app adds a new concept of collections. A Collection is a group of media files that have been imported, curated and annotated into a set within the OA app.

## Entries

An Entry is a unique media item within a Collection and its associated metadata.

## Entry Metadata

Each Entry has an associated set of metadata which can be stored in memory, in a data store (relational, key-value, etc) or serialized to a file. The default format for serialization is JavaScript Object Notation (JSON).

The default metadata fields are:

author:
title (or subject):
description:
dateCreated:
dateImported:
usage (license, terms of use):
location:
tags:
contentType:
contentLength:
originalFileName:
hash (sha256 checksum):

Here is an example JSON format for this metadata:

{"author":"Jay Jonah Jameson","title":"The Amazing Spiderman","dateCreated":"2012-04-23T18:25:43.511Z","contentType":"image/jpeg","contentLength":"987654321"}

## File Naming

In order to improve the ability for receivers of Collections to organize and parse what they are receiving, the OA app can rename media files when they are imported. 

DATE_LOCATION_TITLE_SUBMITTER_WARNING.EXT

DATE: By default htis can be YYYY-MM-DD

LOCATION: For our purposes, Country is the most logical, but any location related data provide that can be encoded appropriately (not GPS coordinates), would be useful here

TITLE: A short summary of the subject, info,etc

SUBMITTER: One or more names representing the person submitting this item

FLAG: An optional keyword indicating a warning to viewers/receivers ("NONE","VIOLENCE","GRAPHIC","ASSAULT")

Example:

2018-11-10_USA_FreeBeer_NFreitas_GRAPHIC.jpg

2018-11-10_USA_FreeBeer_NFreitas_GRAPHIC.jpg.meta.json

## Collection File / Folder Structure

Collection-DATE_LOCATION_TILE_SUBMITTER_WARNING/

 - Collection-DATE_LOCATION_TILE_SUBMITTER_WARNING.meta.json
 - Entry-DATE_LOCATION_TITLE_SUBMITTER_WARNING
    - DATE_LOCATION_TITLE_SUBMITTER_WARNING.jpg
    - DATE_LOCATION_TITLE_SUBMITTER_WARNING.jpg.meta.json