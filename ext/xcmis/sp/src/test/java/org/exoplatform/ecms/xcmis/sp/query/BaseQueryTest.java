/**
 *  Copyright (C) 2003-2010 eXo Platform SAS.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.ecms.xcmis.sp.query;

import org.exoplatform.ecms.xcmis.sp.BaseTest;
import org.exoplatform.ecms.xcmis.sp.PropertyDefinitions;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xcmis.spi.BaseContentStream;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.Storage;
import org.xcmis.spi.model.Property;
import org.xcmis.spi.model.PropertyDefinition;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.model.VersioningState;
import org.xcmis.spi.model.impl.StringProperty;
import org.xcmis.spi.query.Query;
import org.xcmis.spi.query.Result;
import org.xcmis.spi.utils.MimeType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id$
 */
public abstract class BaseQueryTest extends BaseTest
{
   private static final Log LOG = ExoLogger.getLogger(BaseQueryTest.class.getName());

   protected final static String NASA_DOCUMENT = "cmis:nasa-mission";

   protected final static String PROPERTY_BOOSTER = "cmis:booster-name";

   protected final static String PROPERTY_COMMANDER = "cmis:commander";

   protected final static String PROPERTY_COMMAND_MODULE_PILOT = "cmis:command-module-pilot";

   protected final static String PROPERTY_LUNAR_MODULE_PILOT = "cmis:lunar-module-pilot";

   protected final static String PROPERTY_BOOSTER_MASS = "cmis:booster-mass";

   protected final static String PROPERTY_SAMPLE_RETURNED = "cmis:sample-returned";

   protected final static String PROPERTY_STATUS = "cmis:status";

   protected Storage storage;

   protected FolderData rootFolder;

   protected TypeDefinition nasaDocumentTypeDefinition;

   protected TypeDefinition folderTypeDefinition;

   public void setUp() throws Exception
   {
      super.setUp();
      storage = storageProvider.getConnection().getStorage();
      rootFolder = (FolderData)storage.getObjectById(storage.getRepositoryInfo().getRootFolderId());

      nasaDocumentTypeDefinition = storage.getTypeDefinition(NASA_DOCUMENT, true);
      folderTypeDefinition = storage.getTypeDefinition("cmis:folder", true);
   }

   protected DocumentData createDocument(FolderData folder, String name, TypeDefinition typeDefinition, byte[] content,
      MimeType mimeType) throws Exception
   {
      return createDocument(folder, name, typeDefinition, new BaseContentStream(content, null, mimeType), null);
   }

   protected DocumentData createDocument(FolderData folder, String name, TypeDefinition typeDefinition,
      ContentStream content, VersioningState versioningState) throws Exception//   /**
   {

      PropertyDefinition<?> def = PropertyDefinitions.getPropertyDefinition("cmis:document", CmisConstants.NAME);
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(CmisConstants.NAME, new StringProperty(def.getId(), def.getQueryName(), def.getLocalName(), def
         .getDisplayName(), name));

      DocumentData document =
         storage.createDocument(folder, typeDefinition, properties, content, null, null, versioningState == null
            ? VersioningState.MAJOR : versioningState);
      return document;
   }

   protected FolderData createFolder(FolderData folder, String name, TypeDefinition typeDefinition) throws Exception
   {
      PropertyDefinition<?> def = PropertyDefinitions.getPropertyDefinition("cmis:folder", CmisConstants.NAME);
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(CmisConstants.NAME, new StringProperty(def.getId(), def.getQueryName(), def.getLocalName(), def
         .getDisplayName(), name));

      FolderData newFolder = storage.createFolder(folder, typeDefinition, properties, null, null);
      return newFolder;
   }

   protected void checkResult(ItemsIterator<Result> result, String[] columns, String[][] expectedResults)
      throws Exception
   {
      // expected
      Set<String> expectedPaths = new HashSet<String>();
      if (LOG.isDebugEnabled())
      {
         LOG.debug("expected:");
      }
      for (int i = 0; i < expectedResults.length; i++)
      {
         String newRow = "";
         for (int j = 0; j < expectedResults[i].length; j++)
         {
            newRow += "|" + expectedResults[i][j];
         }
         expectedPaths.add(newRow);
         if (LOG.isDebugEnabled())
         {
            LOG.debug(newRow);
         }
      }
      // actual

      Set<String> resultPaths = new HashSet<String>();
      if (LOG.isDebugEnabled())
      {
         LOG.debug("result:");
      }
      while (result.hasNext())
      {
         String objectId = result.next().getObjectId();
         String resultRow = "";
         for (String column : columns)
         {
            resultRow += "|" + getProperty(objectId, column);

         }
         resultPaths.add(resultRow);
         if (LOG.isDebugEnabled())
         {
            LOG.debug(resultRow);
         }
      }

      checkResults(expectedPaths, resultPaths);
   }

   protected void checkResult(String statement, ObjectData[] nodes)
   {
      Query query = new Query(statement, true);

      ItemsIterator<Result> result = storage.query(query);
      checkResult(result, nodes);
   }

   protected void checkResult(ItemsIterator<Result> result, ObjectData[] nodes)
   {
      // collect rows
      Set<String> expectedPaths = new HashSet<String>();
      if (LOG.isDebugEnabled())
      {
         LOG.debug("expected:");
      }

      for (ObjectData node : nodes)
      {
         expectedPaths.add(node.getObjectId());
         if (LOG.isDebugEnabled())
         {
            LOG.debug(node.getObjectId());
         }
      }

      Set<String> resultPaths = new HashSet<String>();
      if (LOG.isDebugEnabled())
      {
         LOG.debug("result:");
      }
      while (result.hasNext())
      {
         Result next = result.next();
         String id = next.getObjectId();
         resultPaths.add(id);
         try
         {
            storage.getObjectById(id);
         }
         catch (ObjectNotFoundException e)
         {
            fail(e.getMessage());
         }
         //LOG.debug("id:=" + id + " path:=" + object.getParent().getPath() + "/" + object.getName());
      }

      checkResults(expectedPaths, resultPaths);
   }

   protected void checkResultOrder(ItemsIterator<Result> result, ObjectData[] nodes)
   {
      // collect rows
      String expectedPaths = "";
      if (LOG.isDebugEnabled())
      {
         LOG.debug("expected:");
      }

      for (ObjectData node : nodes)
      {
         expectedPaths += ":" + node.getObjectId();
         if (LOG.isDebugEnabled())
         {
            LOG.debug(node.getObjectId());
         }
      }

      String resultPaths = "";
      if (LOG.isDebugEnabled())
      {
         LOG.debug("result:");
      }
      while (result.hasNext())
      {
         String id = result.next().getObjectId();
         resultPaths += ":" + id;
         if (LOG.isDebugEnabled())
         {
            LOG.debug(id);
         }
      }

      assertEquals(expectedPaths, resultPaths);
   }

   private void checkResults(Set<String> expectedRowResults, Set<String> actualRowResults)
   {
      // check if all expected are in result
      for (String path : expectedRowResults)
      {
         assertTrue(path + " is not part of the result set " + actualRowResults, actualRowResults.contains(path));
      }
      // check result does not contain more than expected
      for (String path : actualRowResults)
      {
         assertTrue(path + " is not expected to be part of the result set", expectedRowResults.contains(path));
      }
   }

   private String getProperty(String objectId, String propertyId) throws Exception
   {
      ObjectData entry = storage.getObjectById(objectId);
      return entry.getProperty(propertyId).toString();
   }

}
