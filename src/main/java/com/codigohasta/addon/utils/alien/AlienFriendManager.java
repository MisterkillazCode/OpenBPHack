package com.codigohasta.addon.utils.alien;

import java.util.ArrayList;
import java.util.List;

public class AlienFriendManager {
   public final List<String> friendList = new ArrayList<>();

   public boolean isFriend(String name) {
      return name.equals("KizuatoResult") || name.equals("8AI") || this.friendList.contains(name);
   }

   public void add(String name) {
      if (!this.friendList.contains(name)) {
         this.friendList.add(name);
      }
   }

   public void remove(String name) {
      this.friendList.remove(name);
   }
}
