# Open Archive: Space Capsule Specification

Nathan Freitas aka @n8fr8
Guardian Project
21 November 2018

## Summary

This document is for outlining the folder and file structure for the publishing of an Open Archive (OA) collection, otherwise known as a "Space Capsule". This is specifies how to serialize to a disk (or a tar/zip archive file) the set of media files and metadata imported, organized and curated in the Open Archive app. It will also specify the format of the metadata included, and what extra data can also be included, such as OpenPGP signatures.

## Collections

The current development work on the OA app adds a new concept of collections or albums. These are a group of multiple imported media files into a set. 

## Entries

An Entry is a unique media item and its associated metadata.

## Entry Metadata

author, title (subject), description, dateCreated, dateImported, license (usage,terms), location, tags (category, project), mimeType (content type i.e. image/jpeg), hash (sha 256 hash)

{"author":"Nathan F.","createDate":"Oct 17, 2018 4:30:01 PM","description":"aren\u0027t they so orange","licenseUrl":"[https://creativecommons.org/licenses/by/4.0/","location":"pumpkin](https://creativecommons.org/licenses/by/4.0/%22,%22location%22:%22pumpkin)
patch","mediaHash":[],"mimeType":"image/jpeg","originalFilePath":"content://com.android.providers.media.documents/document/image%3A66971","serverUrl":"[https://cloud.guardianproject.info/remote.php/dav/files/n8fr8/OpenArchive/hcd5-kids+love+pumpkins.jpg/jqnr-kids+love+pumpkins.jpg","status":3,"tags":"autumn;;Halloween","title":"kids](https://cloud.guardianproject.info/remote.php/dav/files/n8fr8/OpenArchive/hcd5-kids+love+pumpkins.jpg/jqnr-kids+love+pumpkins.jpg%22,%22status%22:3,%22tags%22:%22autumn;;Halloween%22,%22title%22:%22kids) love pumpkins","id":15}

## File Naming

YYYY-MM-DD_COUNTRY_TITLE_AUTHOR.EXT

## Folder Structure

Collection/

 - CollectionMetadata.json
 - Entry-YYYY-MM-DD_COUNTRY_TITLE_AUTHOR
    - YYYY-MM-DD_COUNTRY_TITLE_AUTHOR.jpg
    - YYYY-MM-DD_COUNTRY_TITLE_AUTHOR.jpg.json